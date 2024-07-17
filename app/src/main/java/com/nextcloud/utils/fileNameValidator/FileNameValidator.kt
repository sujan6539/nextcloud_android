/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.fileNameValidator

import android.content.Context
import android.text.TextUtils
import com.nextcloud.utils.extensions.dot
import com.nextcloud.utils.extensions.removeFileExtension
import com.nextcloud.utils.extensions.space
import com.owncloud.android.R
import com.owncloud.android.lib.resources.status.OCCapability

object FileNameValidator {
    private val reservedWindowsChars = "[<>:\"/\\\\|?*]".toRegex()
    private val reservedUnixChars = "[/<>|:&]".toRegex()
    private val reservedWindowsNames = listOf(
        "CON", "PRN", "AUX", "NUL",
        "COM0", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "COM¹", "COM²", "COM³",
        "LPT0", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
        "LPT¹", "LPT²", "LPT³"
    )
    private val forbiddenFileExtensions = listOf(".filepart", ".part")

    /**
     * Checks the validity of a file name.
     *
     * @param filename The name of the file to validate.
     * @param capability The capabilities affecting the validation criteria such as forbiddenFilenames, forbiddenCharacters.
     * @param context The context used for retrieving error messages.
     * @param existedFileNames Set of existing file names to avoid duplicates.
     * @return An error message if the filename is invalid, null otherwise.
     */
    @Suppress("ReturnCount")
    fun checkFileName(
        filename: String,
        capability: OCCapability,
        context: Context,
        existedFileNames: MutableSet<String>? = null
    ): String? {
        if (TextUtils.isEmpty(filename)) {
            return context.getString(R.string.filename_empty)
        }

        existedFileNames?.let {
            if (isFileNameAlreadyExist(filename, existedFileNames)) {
                return context.getString(R.string.file_already_exists)
            }
        }

        if (filename.endsWith(space()) || filename.endsWith(dot())) {
            return context.getString(R.string.file_name_validator_error_ends_with_space_period)
        }

        checkInvalidCharacters(filename, capability, context)?.let {
            return it
        }

        if (capability.forbiddenFilenames.isTrue &&
            (
                reservedWindowsNames.contains(filename.uppercase()) ||
                    reservedWindowsNames.contains(filename.removeFileExtension().uppercase())
                )
        ) {
            return context.getString(R.string.file_name_validator_error_reserved_names, filename.substringBefore(dot()))
        }

        if (capability.forbiddenFilenameExtension.isTrue && forbiddenFileExtensions.any {
                filename.endsWith(
                    it,
                    ignoreCase = true
                )
            }) {
            return context.getString(
                R.string.file_name_validator_error_forbidden_file_extensions,
                filename.substringAfter(dot())
            )
        }

        return null
    }

    /**
     * Checks the validity of file paths wanted to move or copied inside the folder.
     *
     * @param folderPath Target folder to be used for move or copy.
     * @param filePaths The list of file paths to move or copy to folderPath.
     * @param capability The capabilities affecting the validation criteria.
     * @param context The context used for retrieving error messages.
     * @return True if folder path and file paths are valid, false otherwise.
     */
    fun checkFolderAndFilePaths(
        folderPath: String,
        filePaths: List<String>,
        capability: OCCapability,
        context: Context
    ): Boolean {
        return checkFolderPath(folderPath, capability, context) && checkFilePaths(filePaths, capability, context)
    }

    fun checkFilePaths(filePaths: List<String>, capability: OCCapability, context: Context): Boolean {
        return filePaths.all { checkFileName(it, capability, context) == null }
    }

    fun checkFolderPath(folderPath: String, capability: OCCapability, context: Context): Boolean {
        return folderPath.split("[/\\\\]".toRegex())
            .none { it.isNotEmpty() && checkFileName(it, capability, context) != null }
    }

    @Suppress("ReturnCount")
    private fun checkInvalidCharacters(name: String, capability: OCCapability, context: Context): String? {
        if (capability.forbiddenFilenameCharacters.isFalse) return null

        val invalidCharacter = name.find {
            val input = it.toString()
            input.matches(reservedWindowsChars) || input.matches(reservedUnixChars)
        }

        if (invalidCharacter == null) return null

        return context.getString(R.string.file_name_validator_error_invalid_character, invalidCharacter)
    }

    fun isFileHidden(name: String): Boolean = !TextUtils.isEmpty(name) && name[0] == '.'

    fun isFileNameAlreadyExist(name: String, fileNames: MutableSet<String>): Boolean = fileNames.contains(name)
}
