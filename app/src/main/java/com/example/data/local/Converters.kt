package com.example.data.local

import com.example.data.model.CourseGrade
import com.example.data.model.TemplateSection
import org.json.JSONArray
import org.json.JSONObject

object JsonConverters {
    fun courseGradesToJson(list: List<CourseGrade>): String {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("courseCode", item.courseCode)
            obj.put("courseTitle", item.courseTitle)
            obj.put("creditUnits", item.creditUnits)
            obj.put("courseworkScore", item.courseworkScore)
            obj.put("examScore", item.examScore)
            obj.put("practicalScore", item.practicalScore)
            obj.put("thesisScore", item.thesisScore)
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToCourseGrades(json: String?): List<CourseGrade> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<CourseGrade>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CourseGrade(
                        id = obj.optString("id"),
                        courseCode = obj.optString("courseCode"),
                        courseTitle = obj.optString("courseTitle"),
                        creditUnits = obj.optInt("creditUnits", 3),
                        courseworkScore = obj.optDouble("courseworkScore", 0.0),
                        examScore = obj.optDouble("examScore", 0.0),
                        practicalScore = obj.optDouble("practicalScore", 0.0),
                        thesisScore = obj.optDouble("thesisScore", 0.0)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun templateSectionsToJson(list: List<TemplateSection>): String {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("promptCue", item.promptCue)
            obj.put("defaultText", item.defaultText)
            obj.put("isMandatory", item.isMandatory)
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToTemplateSections(json: String?): List<TemplateSection> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<TemplateSection>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TemplateSection(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        promptCue = obj.optString("promptCue"),
                        defaultText = obj.optString("defaultText"),
                        isMandatory = obj.optBoolean("isMandatory", true)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
