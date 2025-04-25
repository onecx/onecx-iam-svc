package org.tkit.onecx.iam.rs.internal.mappers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.*;
import org.tkit.onecx.iam.domain.model.PageResult;
import org.tkit.onecx.iam.domain.model.UserSearchCriteria;

import gen.org.tkit.onecx.iam.internal.model.UserDTO;
import gen.org.tkit.onecx.iam.internal.model.UserPageResultDTO;
import gen.org.tkit.onecx.iam.internal.model.UserSearchCriteriaDTO;

@Mapper
public interface UserMapper {

    UserSearchCriteria map(UserSearchCriteriaDTO dto);

    @Mapping(target = "removeStreamItem", ignore = true)
    UserPageResultDTO map(PageResult<UserRepresentation> usersPage, @Context String realm);

    @Mapping(target = "removeAttributesItem", ignore = true)
    @Mapping(target = "domain", expression = "java(realm)")
    UserDTO map(UserRepresentation user, @Context String realm);

    default OffsetDateTime map(Long dateTime) {
        if (dateTime == null) {
            return null;
        }
        var tmp = LocalDateTime.ofInstant(Instant.ofEpochMilli(dateTime),
                TimeZone.getDefault().toZoneId());

        return OffsetDateTime.of(tmp, ZoneId.systemDefault().getRules().getOffset(tmp));
    }

    @AfterMapping
    default void removeServiceAccounts(@MappingTarget UserPageResultDTO dto, PageResult<UserRepresentation> usersPage) {
        dto.getStream().removeIf(userDTO -> userDTO.getUsername().startsWith("service-account"));
    }
}
