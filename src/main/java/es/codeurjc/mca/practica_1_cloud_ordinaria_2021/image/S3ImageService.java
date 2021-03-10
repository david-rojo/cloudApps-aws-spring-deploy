package es.codeurjc.mca.practica_1_cloud_ordinaria_2021.image;

import java.io.File;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

@Service("storageService")
@Profile("production")
public class S3ImageService implements ImageService {

	private final Logger logger = LoggerFactory.getLogger(S3ImageService.class);
	
	private AmazonS3 s3;

	@Value("${amazon.s3.region}")
	private String awsRegion;

	@Value("${amazon.s3.bucket-name}")
	private String awsBucketName;
	
	@Value("${amazon.s3.endpoint}")
	private String awsEndpoint;
	
	@PostConstruct
    public void initS3Client() {
        this.s3 = AmazonS3ClientBuilder.standard()
                    .withRegion(Regions.fromName(awsRegion))
                    .build();
        this.createBucketIfNotExist();
        this.printProperties();
    }

	private void createBucketIfNotExist() {
        if(s3.doesBucketExistV2(awsBucketName)) {
            throw new AmazonS3Exception("Bucket name already exist");
        }
        s3.createBucket(awsBucketName);
        logger.info("{} bucket successfully created", awsBucketName);
    }
	
	@Override
	public String createImage(MultipartFile multiPartFile) {
		
		String fileName = "image_" + UUID.randomUUID() + "_" + multiPartFile.getOriginalFilename();
	    String localPath = System.getProperty("java.io.tmpdir") + "/" + fileName;
	    String s3Path = "events/" + fileName;
	    File localFile = new File(localPath);
		logger.info("Trying to upload image {} to bucket {}", fileName, awsBucketName);

		try {
			multiPartFile.transferTo(localFile);
			PutObjectRequest objectRequest = new PutObjectRequest(
	        		awsBucketName,
	        		s3Path,
	                new File(localPath)
	        );
	        objectRequest.setCannedAcl(CannedAccessControlList.PublicRead);
	        s3.putObject(objectRequest);
	        
		} catch (Exception e) {
			String message = "Error trying to upload image";
			logger.error(message);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
		}
        
		localFile.delete();
        String resourceUrl = s3.getUrl(awsBucketName, s3Path).toString();
        logger.info("Image succesfully uploaded with url: {}", resourceUrl);
        return resourceUrl;
	}

	@Override
	public void deleteImage(String image) {
		
		try {		
			String s3key = image.replace(awsEndpoint, "");
			logger.info("Trying to delete image {} in bucket {}", s3key, awsBucketName);
			s3.deleteObject(awsBucketName, s3key);
			logger.info("Image {} successfully deleted", s3key);
		}
		catch (Exception e) {
			String message = "Error trying to delete image";
			logger.error(message);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
		}
	}
		
	private void printProperties() {
		logger.info("");
		logger.info("Amazon S3 configuration:");
		logger.info("region: {}", awsRegion);
		logger.info("bucket: {}", awsBucketName);
		logger.info("endpoint: {}", awsEndpoint);
		logger.info("");
	}

}
