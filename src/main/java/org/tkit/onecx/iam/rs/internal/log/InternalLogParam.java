package org.tkit.onecx.iam.rs.internal.log;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.tkit.quarkus.log.cdi.LogParam;

import gen.org.tkit.onecx.iam.internal.model.RoleSearchCriteriaDTO;
import gen.org.tkit.onecx.iam.internal.model.UserResetPasswordRequestDTO;
import gen.org.tkit.onecx.iam.internal.model.UserRolesSearchRequestDTO;
import gen.org.tkit.onecx.iam.internal.model.UserSearchCriteriaDTO;

@ApplicationScoped
public class InternalLogParam implements LogParam {

    @Override
    public List<Item> getClasses() {
        return List.of(
                item(10, UserResetPasswordRequestDTO.class, x -> UserResetPasswordRequestDTO.class.getSimpleName()),
                item(10, UserRolesSearchRequestDTO.class, x -> {
                    UserRolesSearchRequestDTO d = (UserRolesSearchRequestDTO) x;
                    return UserRolesSearchRequestDTO.class.getSimpleName() + "[" + d.getIssuer() + "]";
                }),
                item(10, UserSearchCriteriaDTO.class, x -> {
                    UserSearchCriteriaDTO d = (UserSearchCriteriaDTO) x;
                    return UserSearchCriteriaDTO.class.getSimpleName() + "[" + d.getPageNumber() + "," + d.getPageSize() + "]";
                }), item(10, RoleSearchCriteriaDTO.class, x -> {
                    RoleSearchCriteriaDTO d = (RoleSearchCriteriaDTO) x;
                    return RoleSearchCriteriaDTO.class.getSimpleName() + "[" + d.getPageNumber() + "," + d.getPageSize() + "]";
                }));
    }
}
