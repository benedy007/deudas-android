package com.benedy.deudas.data.backup

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Backup/restore using Google Drive REST API v3 + drive.appdata space.
 * File: [BackupPayload.BACKUP_FILE_NAME] in the hidden appDataFolder.
 */
class DriveBackupRepository {

    fun uploadBackup(accessToken: String, json: String) {
        val existingId = findBackupFileId(accessToken)
        if (existingId != null) {
            updateMedia(accessToken, existingId, json)
        } else {
            createMultipart(accessToken, json)
        }
    }

    fun downloadBackup(accessToken: String): String {
        val fileId = findBackupFileId(accessToken)
            ?: throw DriveBackupException("No hay respaldo en Google Drive. Crea uno primero.")
        return downloadMedia(accessToken, fileId)
    }

    private fun findBackupFileId(accessToken: String): String? {
        val q = URLEncoder.encode(
            "name = '${BackupPayload.BACKUP_FILE_NAME}' and trashed = false",
            "UTF-8"
        )
        val url =
            "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$q&fields=files(id,name)"
        val body = httpGet(accessToken, url)
        val files = JSONObject(body).optJSONArray("files") ?: return null
        if (files.length() == 0) return null
        return files.getJSONObject(0).getString("id")
    }

    private fun createMultipart(accessToken: String, json: String) {
        val boundary = "----DeudasBoundary${System.currentTimeMillis()}"
        val metadata = JSONObject()
            .put("name", BackupPayload.BACKUP_FILE_NAME)
            .put("mimeType", "application/json")
            .put("parents", org.json.JSONArray().put("appDataFolder"))
            .toString()

        val body = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata)
            append("\r\n--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(json)
            append("\r\n--$boundary--\r\n")
        }

        val conn = open(
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart",
            "POST",
            accessToken
        )
        conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        conn.doOutput = true
        OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
        readOrThrow(conn)
    }

    private fun updateMedia(accessToken: String, fileId: String, json: String) {
        val conn = open(
            "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media",
            "PATCH",
            accessToken
        )
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.doOutput = true
        OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(json) }
        readOrThrow(conn)
    }

    private fun downloadMedia(accessToken: String, fileId: String): String {
        val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
        return httpGet(accessToken, url)
    }

    private fun httpGet(accessToken: String, url: String): String {
        val conn = open(url, "GET", accessToken)
        return readOrThrow(conn)
    }

    private fun open(url: String, method: String, accessToken: String): HttpURLConnection {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Authorization", "Bearer $accessToken")
            connectTimeout = 30_000
            readTimeout = 60_000
        }
        return conn
    }

    private fun readOrThrow(conn: HttpURLConnection): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.use { inp ->
            BufferedReader(InputStreamReader(inp, StandardCharsets.UTF_8)).readText()
        }.orEmpty()
        conn.disconnect()
        if (code !in 200..299) {
            throw DriveBackupException("Drive HTTP $code: ${text.take(300)}")
        }
        return text
    }
}

class DriveBackupException(message: String) : Exception(message)
