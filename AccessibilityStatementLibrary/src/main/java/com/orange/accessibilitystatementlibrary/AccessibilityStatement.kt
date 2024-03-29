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

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccessibilityStatement {
    var referential: String = ""
    var technologies: String = ""
    var date: String? = null
    var resultScore: String? = null
    var title: String = ""

    fun getDateddMMyyyyFormat(): String? {
        val inputPattern = "yyyy-MM-dd"
        var outputPattern = "MMM dd, yyyy"
        if (Locale.getDefault().getDisplayLanguage() == "français") {
            outputPattern = "dd MMMM yyyy"
        }
        val inputFormat = SimpleDateFormat(inputPattern)
        val outputFormat = SimpleDateFormat(outputPattern)
        val inputDate: Date?
        var formattedDate: String? = null
        try {
            inputDate = date?.let { inputFormat.parse(it) }
            if (inputDate != null) {
                formattedDate = outputFormat.format(inputDate)
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return formattedDate
    }
}