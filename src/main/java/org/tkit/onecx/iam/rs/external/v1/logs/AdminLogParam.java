package org.tkit.onecx.iam.rs.external.v1.logs;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.tkit.quarkus.log.cdi.LogParam;

import gen.org.tkit.onecx.iam.v1.model.UserRolesSearchRequestDTOV1;

@ApplicationScoped
public class AdminLogParam implements LogParam {

    @Override
    public List<Item> getClasses() {
        return List.of(
                item(10, UserRolesSearchRequestDTOV1.class, x -> {
                    UserRolesSearchRequestDTOV1 d = (UserRolesSearchRequestDTOV1) x;
                    return UserRolesSearchRequestDTOV1.class.getSimpleName() + "[" + d.getIssuer() + "]";
                }));
    }
}
