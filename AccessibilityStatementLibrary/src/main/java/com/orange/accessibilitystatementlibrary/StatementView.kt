/*
 *
 * Accessibility-Statement-Library
 *
 * Copyright (C) 2022 Orange
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.orange.accessibilitystatementlibrary

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import com.orange.accessibilitystatementlibrary.databinding.ViewStatementBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class StatementView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    ConstraintLayout(context, attrs, defStyleAttr) {

    companion object {

        private const val ACCESSIBILITY_STATEMENT_ASSETS_DIRECTORY = "accessibility-statement"
    }

    var urlAccessibilityDeclaration: String? = null

    var accessibilityStatementFilePath = "accessibility_result.xml"
        set(value) {
            field = value
            displayResults()
        }

    val binding: ViewStatementBinding

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var isAssetsCopyFinished = false

    init {
        binding = ViewStatementBinding.inflate(LayoutInflater.from(context), this)
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.DeclarationView)
        attributes.getString(R.styleable.DeclarationView_file_path)?.let { accessibilityStatementFilePath = it }
        displayResults()
        initMoreDetailsButton(attributes)
        coroutineScope.launch {
            copyAssetsToFilesDirectory(context)
        }
    }

    private fun initMoreDetailsButton(attributes: TypedArray) {
        try {
            val text = attributes.getString(R.styleable.DeclarationView_declarant)
            urlAccessibilityDeclaration =
                attributes.getString(R.styleable.DeclarationView_details_url)
            binding.declarantTextView.text = text

            binding.buttonSeeMore.setOnClickListener {
                val intent = if (urlAccessibilityDeclaration != null) {
                    Intent(Intent.ACTION_VIEW, Uri.parse(urlAccessibilityDeclaration))
                } else {
                    getAccessibilityStatementAssetsIntent(context)
                }
                intent?.let { context.startActivity(intent) }
            }
        } finally {
            attributes.recycle()
        }
    }

    private fun displayResults() {
        val statementXMLParser = StatementXMLParser(context, accessibilityStatementFilePath)
        val accessibilityStatement = statementXMLParser.getAccessibilityStatementFromXML()

        val resultPercentValue = accessibilityStatement?.resultScore?.toInt() ?: 0
        binding.resultTextView.text = context.getString(R.string.result, resultPercentValue)
        binding.resultTextView.contentDescription =
            context.getString(R.string.result_content_desc, resultPercentValue)

        binding.resultProgresBar.max = 100
        binding.resultProgresBar.progress = resultPercentValue

        binding.dateTextView.text = accessibilityStatement?.getDateddMMyyyyFormat()
        binding.referentialTextView.text = accessibilityStatement?.referential
        binding.technologieTextView.text = accessibilityStatement?.technologies

        displayComplianceStatus(resultPercentValue, accessibilityStatement?.title)
    }

    private fun displayComplianceStatus(percentValue: Int, title: String?) {
        var nameOfApplication = title
        if (nameOfApplication == null || nameOfApplication.length == 0) {
            nameOfApplication = context.getString(R.string.nameOfApp)
        }
        binding.resultDetailTextView.text = context.getString(R.string.conformity_state, nameOfApplication, percentValue)
    }

    private fun copyAssetsToFilesDirectory(context: Context) {
        val filesDirPath = context.filesDir.absolutePath

        // Create destination path
        with(File("$filesDirPath/$ACCESSIBILITY_STATEMENT_ASSETS_DIRECTORY")) {
            if (!exists()) {
                mkdirs()
            }
        }

        // Copy assets to destination
        getAccessibilityStatementAssetsPaths(context).forEach { path ->
            context.assets.open(path).use { inputSteam ->
                val file = File(filesDirPath, path)
                if (!file.exists()) {
                    file.outputStream().use { outputStream ->
                        inputSteam.copyTo(outputStream, 1024)
                    }
                }
            }
        }

        isAssetsCopyFinished = true
    }

    private fun getAccessibilityStatementAssetsIntent(context: Context): Intent? {
        val uris = getAccessibilityStatementAssetsPaths(context).mapNotNull { path ->
            val file = File(context.filesDir, path)
            val authority = "${context.packageName}.accessibilitystatementfileprovider"
            FileProvider.getUriForFile(context, authority, file)
        }

        val htmlURI = uris.firstOrNull { it.isHtmlFile }
        val intent = if (htmlURI != null && isAssetsCopyFinished) {
            // Add other files (css, images, etc...) to a clip data
            var clipData: ClipData? = null
            uris.filter { !it.isHtmlFile }
                .forEach { uri ->
                    if (clipData == null) {
                        clipData = ClipData.newRawUri("", uri)
                    } else {
                        clipData?.addItem(ClipData.Item(uri))
                    }
                }

            Intent(Intent.ACTION_VIEW, htmlURI).apply {
                this.clipData = clipData
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            null
        }

        return intent
    }

    private fun getAccessibilityStatementAssetsPaths(context: Context): List<String> {
        return with(ACCESSIBILITY_STATEMENT_ASSETS_DIRECTORY) {
            context.assets.list(this).orEmpty().map { "$this/$it" }
        }
    }

    private val Uri.isHtmlFile: Boolean
        get() = path?.endsWith(".html") == true
}