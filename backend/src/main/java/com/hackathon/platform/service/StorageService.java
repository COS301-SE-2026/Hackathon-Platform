package com.hackathon.platform.service;
 
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
 
/**
 * Abstraction layer for object storage operations.
 * Programming against this interface allows swapping the storage
 * provider without changing any controller or
 * business logic code.
 */
public interface StorageService {
 
  /**
   * Uploads a multipart file to the specified container at the given storage key path.
   *
   * @param containerName the Azure blob container name
   * @param storageKey    full storage key path constructed via BlobPath helpers
   * @param file          the multipart file from the HTTP request
   * @return the raw blob URL (store the storageKey in the database, not this URL)
   */
  String upload(String containerName, String storageKey, MultipartFile file);
 
  /**
   * Uploads raw bytes to the specified container (used internally by scoring workers).
   *
   * @param containerName the Azure blob container name
   * @param storageKey    full storage key path constructed via BlobPath helpers
   * @param data          the byte array to upload
   * @param contentType   the MIME type of the content
   * @return the raw blob URL
   */
  String uploadBytes(String containerName, String storageKey, byte[] data, String contentType);
 
  /**
   * Generates a time-limited presigned SAS URL for read access to a blob.
   * Always use presigned URLs when serving files to clients.
   *
   * @param containerName the Azure blob container name
   * @param storageKey    full storage key path
   * @param expiryMinutes how long the URL remains valid
   * @return presigned SAS URL
   */
  String generatePresignedUrl(String containerName, String storageKey, int expiryMinutes);
 
  /**
   * Opens an InputStream for a blob (used internally by the scoring worker).
   *
   * @param containerName the Azure blob container name
   * @param storageKey    full storage key path
   * @return InputStream of the blob content
   */
  InputStream download(String containerName, String storageKey);
 
  /**
   * Deletes a blob from storage.
   *
   * @param containerName the Azure blob container name
   * @param storageKey    full storage key path
   */
  void delete(String containerName, String storageKey);
 
  /**
   * Checks whether a blob exists in the specified container.
   *
   * @param containerName the Azure blob container name
   * @param storageKey    full storage key path
   * @return true if the blob exists, false otherwise
   */
  boolean exists(String containerName, String storageKey);
  
}
