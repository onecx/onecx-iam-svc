package org.tkit.onecx.iam.rs.external.v1.mappers;

import java.util.List;

import org.keycloak.representations.idm.RoleRepresentation;
import org.mapstruct.Mapper;

import gen.org.tkit.onecx.iam.v1.model.RoleDTOV1;
import gen.org.tkit.onecx.iam.v1.model.UserRolesResponseDTOV1;

@Mapper
public interface RoleMapper {

    RoleDTOV1 map(RoleRepresentation user);

    default UserRolesResponseDTOV1 map(List<RoleRepresentation> userRoles) {
        UserRolesResponseDTOV1 responseDTO = new UserRolesResponseDTOV1();
        responseDTO.setRoles(mapRoles(userRoles));
        return responseDTO;
    }

    List<RoleDTOV1> mapRoles(List<RoleRepresentation> roles);

}
