package es.codeurjc.mca.practica_1_cloud_ordinaria_2021.image;

import java.io.File;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

@Service("storageService")
@Profile("production")
public class RemoteImageService implements ImageService {

	private final Logger logger = LoggerFactory.getLogger(RemoteImageService.class);
	
	public static AmazonS3 s3;

	private String awsRegion;

	private String awsBucketName;

	public RemoteImageService(
			@Value("${amazon.s3.region}") String awsRegion,
			@Value("${amazon.s3.bucket-name}") String awsBucketName) {
		
		this.awsRegion = awsRegion;
		this.awsBucketName = awsBucketName;
		s3 = AmazonS3ClientBuilder
				.standard()
				.withRegion(this.awsRegion)
				.build();
		this.printProperties();
	}

	@Override
	public String createImage(MultipartFile multiPartFile) {
		
		String fileName = "image_" + UUID.randomUUID() + "_" + multiPartFile.getOriginalFilename();
	    String path = "events/"+ fileName;
		logger.info("Trying to upload image {} to bucket {}", fileName, awsBucketName);
		
		if (!this.existsBucket()) {
	    	logger.info("{} bucket don't exist", awsBucketName);
	    	this.createBucket();
	    }

        PutObjectRequest objectRequest = new PutObjectRequest(
        		awsBucketName,
                fileName,
                new File(path)
        );
        objectRequest.setCannedAcl(CannedAccessControlList.PublicRead);
        s3.putObject(objectRequest);
        
        String resourceUrl = s3.getUrl(awsBucketName, path).toString();
        logger.info("Image succesfully uploaded with url: {}", resourceUrl);
        return resourceUrl;
	}

	@Override
	public void deleteImage(String image) {
		logger.info("Trying to delete image {} in bucket {}", image, awsBucketName);
		s3.deleteObject(awsBucketName, image);
		logger.info("Image {} successfully deleted", image);
	}
	
	private boolean existsBucket() {
		return s3.doesBucketExistV2(awsBucketName);
	}
	
	private void createBucket() {
        if(s3.doesBucketExistV2(awsBucketName)) {
            throw new AmazonS3Exception("Bucket name already exist");
        }
        s3.createBucket(awsBucketName);
        logger.info("{} bucket successfully created", awsBucketName);
    }
	
	private void printProperties() {
		logger.info("");
		logger.info("Amazon S3 configuration:");
		logger.info("region: {}", awsRegion);
		logger.info("bucket: {}", awsBucketName);
		logger.info("");
	}

}
