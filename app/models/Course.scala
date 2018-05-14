package models

import java.sql.Timestamp

case class Course(id: Long, userId: Long, title: String, createdAt: Timestamp, updateAt: Timestamp)
