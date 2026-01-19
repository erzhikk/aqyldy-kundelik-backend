package kz.aqyldykundelik.schedule.api

import kz.aqyldykundelik.schedule.api.dto.ClassScheduleDto

class ScheduleActivationException(
    val scheduleDto: ClassScheduleDto
) : RuntimeException("Cannot activate schedule: critical conflicts exist")
