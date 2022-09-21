package com.broto.shareplay

class Utility {
    companion object {
        fun getTimeFormattedString(hours: Int, mins: Int, secs: Int): String {
            if (hours == 0) {
                return String.format("%02d : %02d", mins, secs)
            }
            return "00 : 00"
        }
        fun getFormattedStringFromTimeStamp(t: Int): String {
            var timeStamp = t
            val sec = timeStamp % 60
            timeStamp /= 60
            val min = timeStamp % 60
            timeStamp /= 60
            val hours = timeStamp % 60
            return getTimeFormattedString(hours, min, sec)
        }
    }
}