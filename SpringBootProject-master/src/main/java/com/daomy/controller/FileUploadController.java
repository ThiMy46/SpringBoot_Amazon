package com.daomy.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import java.util.Date;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;

@Controller
public class FileUploadController {


	/** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
        System.getProperty("user.home"), ".credentials/drive-java-quickstart");
    
	private AmazonS3 s3client;
    
    @Value("${cloud.aws.s3.endpointUrl}")
    private String endpointUrl;
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    @Value("${cloud.aws.credentials.accessKey}")
    private String accessKey;
    @Value("${cloud.aws.credentials.secretKey}")
    private String secretKey;
    
    @PostConstruct
    private void initializeAmazon() {
        AWSCredentials credentials = new BasicAWSCredentials(this.accessKey, this.secretKey);
        this.s3client = new AmazonS3Client(credentials);
    }
    /*
    @GetMapping("/files/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

		Resource file = storageService.loadAsResource(filename);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
				.body(file);
	}*/
    
	//Lấy thông tin file và thực hiện việc upload
	@PostMapping("/upload-file")
	public String handleFileUpload(@RequestParam("file") MultipartFile file,@RequestParam("news_id") String news_id,
			@RequestParam("title") String title,
			@RequestParam("content") String content,
			HttpServletRequest request) throws Exception  {
 
		String url=uploadFile(file);
		request.setAttribute("title",title);
		request.setAttribute("content",content+"<a href='"+url+"'>link document</a>");
        //return "redirect:/";
		return "NewNews";
        
	}
	public String uploadFile(MultipartFile multipartFile) {
    	//đường dẫn đến thư mục chứa ảnh trên AMAZON
        String fileUrl = "";
        try {
            File file = convertMultiPartToFile(multipartFile);
            String fileName = generateFileName(multipartFile);
            fileUrl = endpointUrl + "/" + bucketName + "/" + fileName;
            uploadFileTos3bucket(fileName, file);
            file.delete();
        } catch (Exception e) {
           e.printStackTrace();
        }
        return fileUrl;
    }

	//chuyển đổi MultipartFile sang File
    private File convertMultiPartToFile(MultipartFile file) throws IOException {
    	File convFile = new File(file.getOriginalFilename());
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }
    private String generateFileName(MultipartFile multiPart) {
        return new Date().getTime() + "";
    }

    private void uploadFileTos3bucket(String fileName, File file) {
        s3client.putObject(new PutObjectRequest(bucketName, fileName, file)
                .withCannedAcl(CannedAccessControlList.PublicRead));//chia sẽ quyền
    }

    public String deleteFileFromS3Bucket(String fileUrl) {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        s3client.deleteObject(new DeleteObjectRequest(bucketName, fileName));
        return "Successfully deleted";
    }

}