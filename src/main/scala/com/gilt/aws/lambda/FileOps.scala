package com.gilt.aws.lambda

import java.io.{File, RandomAccessFile}
import java.nio.ByteBuffer

import software.amazon.awssdk.core.SdkBytes

object FileOps {

  def fileToBuffer(
    file: File,
  ): ByteBuffer = {
    val buffer = ByteBuffer.allocate(file.length().toInt)
    val aFile = new RandomAccessFile(file, "r")
    val inChannel = aFile.getChannel()
    while (inChannel.read(buffer) > 0) {}
    inChannel.close()
    buffer.rewind()
    buffer
  }

}
