package com.mangareader.pagesmanagementservice.service.impl;

import static org.apache.commons.codec.binary.Base64.decodeBase64;

import com.mangareader.pagesmanagementservice.controller.request.AddPageRequest;
import com.mangareader.pagesmanagementservice.service.PageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.MinioException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.messages.Item;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PageServiceImpl implements PageService {
  private final MinioClient s3Client;
  @Value("${s3.part.size}")
  private int partSize;

  @Override
  public void addPage(AddPageRequest request) {
    try {
      if (isBucketExists(request)) {
        putObject(request);
      } else {
        makeBucket(request);
        putObject(request);
      }
    } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
      log.error("Saving page to s3 has failed request {}", request);
    }
  }


  @Override
  public Map<Long, String> getPages(String titleId, String chapterNumber, String pageNumber,
                                    String pageSize) {
    int page = Integer.parseInt(pageNumber);
    int size = Integer.parseInt(pageSize);
    int skip = (page - 1) * size;
    String bucketName = titleId + "-" + chapterNumber;

    Iterable<Result<Item>> listObjects = s3Client.listObjects(
        ListObjectsArgs.builder()
            .bucket(bucketName)
            .recursive(true)
            .build()
    );

    return StreamSupport.stream(listObjects.spliterator(), false)
        .skip(skip)
        .limit(size)
        .map(result -> processItem(result, bucketName))  // Передаем `bucketName` в `processItem`
        .filter(Objects::nonNull)  // Исключаем неудачные операции
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Map.Entry<Long, String> processItem(Result<Item> result, String bucketName) {
    try {
      Item item = result.get();
      String objectName = item.objectName();

      // Преобразуем имя объекта в номер страницы
      Long pageNumberKey = Long.parseLong(objectName.substring(objectName.lastIndexOf('-') + 1));

      // Получаем объект в виде InputStream
      try (InputStream inputStream = s3Client.getObject(
          GetObjectArgs.builder()
              .bucket(bucketName)  // Используем переданный `bucketName`
              .object(objectName)
              .build()
      )) {
        // Кодируем изображение в Base64
        byte[] bytes = inputStream.readAllBytes();
        String base64Image = Base64.getEncoder().encodeToString(bytes);
        return Map.entry(pageNumberKey, base64Image);
      }
    } catch (Exception e) {
      log.error("Error processing item: " + e.getMessage());
      return null;
    }
  }


  private void putObject(AddPageRequest request)
      throws ErrorResponseException, InsufficientDataException, InternalException,
      InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
      ServerException, XmlParserException {
    byte[] pageBytes =
        decodeBase64((request.getData().substring(request.getData().indexOf(",") + 1)).getBytes());
    InputStream fis = new ByteArrayInputStream(pageBytes);

    s3Client.putObject(
        PutObjectArgs.builder().bucket(getBucketName(request))
            .object(request.getPageNumber().toString())
            .stream(fis, pageBytes.length, partSize).build());
  }

  private static @NotNull String getBucketName(AddPageRequest request) {
    return request.getMangaId().toString() + "-" + request.getChapterNumber().toString();
  }

  private void makeBucket(AddPageRequest request)
      throws ErrorResponseException, InsufficientDataException, InternalException,
      InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
      ServerException, XmlParserException {
    s3Client.makeBucket(MakeBucketArgs.builder().bucket(getBucketName(request))
        .build());
  }

  private boolean isBucketExists(AddPageRequest request)
      throws ErrorResponseException, InsufficientDataException, InternalException,
      InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
      ServerException, XmlParserException {
    return s3Client.bucketExists(BucketExistsArgs.builder().bucket(getBucketName(request))
        .build());
  }
}
