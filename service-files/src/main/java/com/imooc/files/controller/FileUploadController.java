package com.imooc.files.controller;

import com.imooc.api.controller.files.FileUploaderControllerApi;
import com.imooc.pojo.bo.NewAdminBO;
import com.imooc.exception.GraceException;
import com.imooc.files.resource.FileResource;
import com.imooc.files.service.UploadService;
import com.imooc.grace.result.GraceJSONResult;
import com.imooc.grace.result.ResponseStatusEnum;
import com.imooc.utils.FileUtils;
import com.imooc.utils.extend.AliImageReviewUtils;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import sun.misc.BASE64Decoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: news
 * @description:
 * @author: xiaokaixin
 * @create: 2022-06-30 20:36
 **/
@RestController
public class FileUploadController implements FileUploaderControllerApi {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    @Autowired
    private UploadService uploaderService;

    @Autowired
    private FileResource fileResource;

    @Autowired
    AliImageReviewUtils aliImageReviewUtils;

    @Autowired
    private GridFSBucket gridFSBucket;

    public static final String FAILED_IMAGE_URL = "https://news-xiaokaixin.oss-cn-shanghai.aliyuncs.com/3debb8b67dda4a38.jpg";


    /**
     * 上传头像
     *
     * @param userId
     * @param file
     * @return
     * @throws Exception
     */
    @Override
    public GraceJSONResult uploadFace(String userId, MultipartFile file) throws Exception {
        String path = "";
        if (file != null) {
            // 获得文件上传的名称
            String fileName = file.getOriginalFilename();

            // 判断文件名不能为空
            if (StringUtils.isNotBlank(fileName)) {
                String fileNameArr[] = fileName.split("\\.");
                // 获得后缀
                String suffix = fileNameArr[fileNameArr.length - 1];
                // 判断后缀符合我们的预定义规范
                if (!suffix.equalsIgnoreCase("png") &&
                        !suffix.equalsIgnoreCase("jpg") &&
                        !suffix.equalsIgnoreCase("jpeg")
                ) {
                    return GraceJSONResult.errorCustom(ResponseStatusEnum.FILE_FORMATTER_FAILD);
                }

                // 执行上传
                // path = uploaderService.uploadFdfs(file, suffix);
                path = uploaderService.uploadOSS(file, userId, suffix);

            } else {
                return GraceJSONResult.errorCustom(ResponseStatusEnum.FILE_UPLOAD_NULL_ERROR);
            }
        } else {
            return GraceJSONResult.errorCustom(ResponseStatusEnum.FILE_UPLOAD_NULL_ERROR);
        }

        logger.info("path = " + path);

        String finalPath = "";
        if (StringUtils.isNotBlank(path)) {
            // finalPath = fileResource.getHost() + path;
            finalPath = fileResource.getOssHost() + path;
        } else {
            return GraceJSONResult.errorCustom(ResponseStatusEnum.FILE_UPLOAD_FAILD);
        }

         // 图片审核
        // return GraceJSONResult.ok(doAliImageReview(finalPath));
        return GraceJSONResult.ok(finalPath);
    }


    /**
     * 多文件上传
     * @param userId
     * @param files
     * @return
     * @throws Exception
     */
    @Override
    public GraceJSONResult uploadSomeFiles(String userId, MultipartFile[] files) throws Exception {

        // 声明list，用于存放多个图片的地址路径，返回到前端
        List<String> imageUrlList = new ArrayList<>();
        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                String path = "";
                if (file != null) {
                    // 获得文件上传的名称
                    String fileName = file.getOriginalFilename();

                    // 判断文件名不能为空
                    if (StringUtils.isNotBlank(fileName)) {
                        String fileNameArr[] = fileName.split("\\.");
                        // 获得后缀
                        String suffix = fileNameArr[fileNameArr.length - 1];
                        // 判断后缀符合我们的预定义规范
                        if (!suffix.equalsIgnoreCase("png") &&
                                !suffix.equalsIgnoreCase("jpg") &&
                                !suffix.equalsIgnoreCase("jpeg")
                        ) {
                            continue;
                        }

                        // 执行上传
//                        path = uploaderService.uploadFdfs(file, suffix);
                        path = uploaderService.uploadOSS(file, userId, suffix);

                    } else {
                        continue;
                    }
                } else {
                    continue;
                }

                String finalPath = "";
                if (StringUtils.isNotBlank(path)) {
//                    finalPath = fileResource.getHost() + path;
                    finalPath = fileResource.getOssHost() + path;
                    // FIXME: 放入到imagelist之前，需要对图片做一次审核
                    imageUrlList.add(finalPath);
                } else {
                    continue;
                }
            }
        }

        return GraceJSONResult.ok(imageUrlList);
    }

    /**
     * mongdb上传头像
     * @param newAdminBO
     * @return
     * @throws Exception
     */

    @Override
    public GraceJSONResult uploadToGridFS(NewAdminBO newAdminBO)
            throws Exception {

        // 获得图片的base64字符串
        String file64 = newAdminBO.getImg64();

        // 将base64字符串转换为byte数组
        byte[] bytes = new BASE64Decoder().decodeBuffer(file64.trim());

        // 转换为输入流
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

        // 上传到gridfs中
        ObjectId fileId = gridFSBucket.uploadFromStream(newAdminBO.getUsername() + ".png", inputStream);

        // 获得文件在gridfs中的主键id
        String fileIdStr = fileId.toString();

        return GraceJSONResult.ok(fileIdStr);
    }

    /**
     * 从gridfs中读取图片内容
     * @param faceId
     * @param request
     * @param response
     * @throws Exception
     */
    @Override
    public void readInGridFS(String faceId, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 0. 判断参数
        if (StringUtils.isBlank(faceId) || faceId.equalsIgnoreCase("null")) {
            GraceException.display(ResponseStatusEnum.FILE_NOT_EXIST_ERROR);
        }

        // 1. 从gridfs中读取
        File adminFace = readGridFSByFaceId(faceId);

        // 2. 把人脸图片输出到浏览器
        FileUtils.downloadFileByStream(response, adminFace);
    }

    /**
     * 从gridfs读取内容
     * @param faceId
     * @return
     * @throws Exception
     */
    private File readGridFSByFaceId(String faceId) throws Exception {

        GridFSFindIterable gridFSFiles
                = gridFSBucket.find(Filters.eq("_id", new ObjectId(faceId)));

        GridFSFile gridFS = gridFSFiles.first();

        if (gridFS == null) {
            GraceException.display(ResponseStatusEnum.FILE_NOT_EXIST_ERROR);
        }

        String fileName = gridFS.getFilename();
        System.out.println(fileName);

        // 获取文件流，保存文件到本地或者服务器的临时目录
        File fileTemp = new File("/Users/xiaokaixin/Desktop/imooc-news/temp_face");
        if (!fileTemp.exists()) {
            fileTemp.mkdirs();
        }

        File myFile = new File("/Users/xiaokaixin/Desktop/imooc-news/temp_face/" + fileName);

        // 创建文件输出流
        OutputStream os = new FileOutputStream(myFile);
        // 下载到服务器或者本地
        gridFSBucket.downloadToStream(new ObjectId(faceId), os);

        return myFile;
    }

    /**
     * 头像检查
     * @param pendingImageUrl
     * @return
     */
    private String doAliImageReview(String pendingImageUrl) {

        /**
         * fastdfs 默认存在于内网，无法被阿里云内容管理服务检查到
         * 需要配置到公网才行：
         * 1. 内网穿透，natppp/花生壳/ngrok
         * 2. 路由配置端口映射
         * 3. fdfs 发布到云服务器
         */
        boolean result = false;
        try {
            result = aliImageReviewUtils.reviewImage(pendingImageUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!result) {
            return FAILED_IMAGE_URL;
        }

        return pendingImageUrl;
    }

    @Override
    public GraceJSONResult readFace64InGridFS(String faceId, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 0. 获得gridfs中人脸文件
        File myface = readGridFSByFaceId(faceId);

        // 1. 转换人脸为base64
        String base64Face = FileUtils.fileToBase64(myface);

        return GraceJSONResult.ok(base64Face);
    }
}
