package com.imooc.files.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @program: news
 * @description:
 * @author: xiaokaixin
 * @create: 2022-06-28 21:28
 **/

public interface UploadService {

    /**
     * 使用fastdfs上传文件
     */
    public String uploadFdfs(MultipartFile file, String fileExtName) throws IOException;


    /**
     * 使用OSS上传文件
     */
    public String uploadOSS(MultipartFile file,
                            String userId,
                            String fileExtName) throws Exception;
}
