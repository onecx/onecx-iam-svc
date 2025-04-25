package org.tkit.onecx.iam.rs.internal.mappers;

import java.util.List;

import org.keycloak.representations.idm.RoleRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.onecx.iam.domain.model.PageResult;
import org.tkit.onecx.iam.domain.model.RoleSearchCriteria;

import gen.org.tkit.onecx.iam.internal.model.RoleDTO;
import gen.org.tkit.onecx.iam.internal.model.RolePageResultDTO;
import gen.org.tkit.onecx.iam.internal.model.RoleSearchCriteriaDTO;
import gen.org.tkit.onecx.iam.internal.model.UserRolesResponseDTO;

@Mapper
public interface RoleMapper {
    RoleSearchCriteria map(RoleSearchCriteriaDTO roleSearchCriteriaDTO);

    @Mapping(target = "removeStreamItem", ignore = true)
    RolePageResultDTO map(PageResult<RoleRepresentation> result);

    default UserRolesResponseDTO map(List<RoleRepresentation> userRoles) {
        UserRolesResponseDTO responseDTO = new UserRolesResponseDTO();
        responseDTO.setRoles(mapRoles(userRoles));
        return responseDTO;
    }

    List<RoleDTO> mapRoles(List<RoleRepresentation> roles);

}
