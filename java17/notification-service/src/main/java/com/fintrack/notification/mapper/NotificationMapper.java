package com.fintrack.notification.mapper;

import com.fintrack.notification.dto.NotificationResponse;
import com.fintrack.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotificationMapper {
    NotificationResponse toResponse(Notification n);
}
