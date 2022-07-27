package com.imooc.files.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.imooc.files.resource.FileResource;
import com.imooc.files.service.UploadService;
import org.n3r.idworker.Sid;
import com.imooc.utils.extend.AliyunResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * @program: news
 * @description:
 * @author: xiaokaixin
 * @create: 2022-06-28 21:28
 **/
@Service
public class UploadServiceImpl implements UploadService {


    @Autowired
    public FastFileStorageClient fastFileStorageClient;

    @Autowired
    public FileResource fileResource;

    @Autowired
    public AliyunResource aliyunResource;


    @Autowired
    public Sid sid;


    /**
     * dffs
     * @param file
     * @param fileExtName
     * @return
     * @throws IOException
     */
    @Override
    public String uploadFdfs(MultipartFile file, String fileExtName) throws IOException {
        InputStream inputStream = file.getInputStream();


        StorePath storePath = fastFileStorageClient.uploadFile(inputStream,
                file.getSize(),
                fileExtName,
                null);
        inputStream.close();

        return storePath.getFullPath();
    }


    /**
     * oss
     * @param file
     * @param userId
     * @param fileExtName
     * @return
     * @throws Exception
     */
    @Override
    public String uploadOSS(MultipartFile file,
                            String userId,
                            String fileExtName) throws Exception {

        // Endpoint以杭州为例，其它Region请按实际情况填写。
        String endpoint = fileResource.getEndpoint();
        // 阿里云主账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建RAM账号。
        String accessKeyId = aliyunResource.getAccessKeyID();
        String accessKeySecret = aliyunResource.getAccessKeySecret();

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint,
                accessKeyId,
                accessKeySecret);
//        images/abc/10010/dog.png

        String fileName = sid.nextShort();
        String myObjectName = fileResource.getObjectName()
                + "/" + userId + "/" + fileName + "." + fileExtName;

        // 上传网络流。
        InputStream inputStream = file.getInputStream();
        ossClient.putObject(fileResource.getBucketName(),
                myObjectName,
                inputStream);

        // 关闭OSSClient。
        ossClient.shutdown();

        return myObjectName;
    }
}
