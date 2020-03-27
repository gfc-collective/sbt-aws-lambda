package com.gilt.aws.lambda

import java.nio.ByteBuffer
import java.nio.file.{ Files, Path, StandardOpenOption }
import java.util.zip._

import scala.annotation.tailrec
import scala.collection.JavaConverters._

object FileUtils {

  def packageDir(src: Path, dest: Path): Unit = {
    if (!Files.isDirectory(src)) throw new IllegalArgumentException(s"Source should be directory. src = $src")
    val now = System.currentTimeMillis()
    val emptyCRC = new CRC32().getValue
    val zo = new ZipOutputStream(Files.newOutputStream(dest, StandardOpenOption.CREATE))
    zo.setLevel(Deflater.DEFAULT_COMPRESSION)
    def addDirectoryEntry(relative: Path): Unit = {
      def addTrailingSlash(text: String) = if (text.endsWith("/")) text else text + "/"
      val ze = new ZipEntry(addTrailingSlash(relative.toString))
      ze.setTime(now)
      ze.setSize(0)
      ze.setMethod(ZipEntry.STORED)
      ze.setCrc(emptyCRC)
      zo.putNextEntry(ze)
      zo.closeEntry()
    }
    val readBuffer = ByteBuffer.allocate(8192)
    def addFileEntry(file: Path, relative: Path): Unit = {
      val ze = new ZipEntry(relative.toString)
      ze.setTime(Files.getLastModifiedTime(file).toMillis)
      zo.putNextEntry(ze)
      val bc = Files.newByteChannel(file)
      try {
        @tailrec def read(): Unit = {
          readBuffer.clear()
          val byteCount = bc.read(readBuffer)
          if (byteCount >= 0) {
            zo.write(readBuffer.array(), 0, byteCount)
            read()
          }
        }
        read()
      } finally {
        bc.close()
      }
      zo.closeEntry()
    }
    val fileIterator = Files.walk(src).iterator().asScala
    try {
      for {
        file <- fileIterator
        if file != src
        relative = src.relativize(file)
      } {
        if (Files.isDirectory(file)) addDirectoryEntry(relative)
        else addFileEntry(file, relative)
      }
    } finally {
      zo.close()
    }
  }
}
