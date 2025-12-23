package com.hms.hr_service.mappers;

import com.hms.common.mappers.GenericMapper;
import com.hms.hr_service.dtos.schedule.ScheduleRequest;
import com.hms.hr_service.dtos.schedule.ScheduleResponse;
import com.hms.hr_service.entities.EmployeeSchedule;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ScheduleMapper extends GenericMapper<EmployeeSchedule, ScheduleRequest, ScheduleResponse> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    EmployeeSchedule requestToEntity(ScheduleRequest request);

    @Override
    @Mapping(target = "employee", ignore = true) // Enriched in hook
    ScheduleResponse entityToResponse(EmployeeSchedule entity);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void partialUpdate(ScheduleRequest request, @MappingTarget EmployeeSchedule entity);
}
