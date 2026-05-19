package com.hackathon.platform.storage;

/**
 * Runtime exception thrown when any Azure Blob Storage operation fails.
 * decouple storage errors from the rest of the application.
 */
public class StorageException extends RuntimeException {


  /**
   * Constructs a StorageException with a descriptive message.
   *
   * @param message description of the failure
   */
  public StorageException(String message) {
    super(message);
  }

  /**
   * Constructs a StorageException with a descriptive message and the root cause.
   *
   * @param message description of the failure
   * @param cause   the underlying exception
   */
  public StorageException(String message, Throwable cause) {
    super(message, cause);
  }

  
}