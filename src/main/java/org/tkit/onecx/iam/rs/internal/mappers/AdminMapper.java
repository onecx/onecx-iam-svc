package org.tkit.onecx.iam.rs.internal.mappers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.*;
import org.tkit.onecx.iam.domain.config.KcConfig;
import org.tkit.onecx.iam.domain.model.PageResult;
import org.tkit.onecx.iam.domain.model.RoleSearchCriteria;
import org.tkit.onecx.iam.domain.model.UserSearchCriteria;
import org.tkit.onecx.iam.domain.service.keycloak.KeycloakUtil;

import gen.org.tkit.onecx.iam.internal.model.*;

@Mapper
public interface AdminMapper {

    default ProvidersResponseDTO map(Map<String, KcConfig.KeycloakClientConfig> kcs,
            Map<String, List<RealmRepresentation>> realms, String tokenProviderKey) {
        ProvidersResponseDTO result = new ProvidersResponseDTO();
        if (kcs == null) {
            return result;
        }
        kcs.forEach((s, clientConfig) -> {
            var domains = createDomains(realms.get(s), s, clientConfig.issuerHost());
            result.addProvidersItem(createProvider(s, clientConfig.description().orElse(null), clientConfig.displayName(),
                    tokenProviderKey.equals(s), domains));
        });
        return result;
    }

    default List<DomainDTO> createDomains(List<RealmRepresentation> realms, String name, String issuerHost) {
        if (realms == null) {
            return List.of();
        }
        List<DomainDTO> domains = new ArrayList<>();
        realms.forEach(r -> domains.add(createDomain(r, KeycloakUtil.buildIssuerFromHostAndDomain(issuerHost, r.getRealm()))));
        return domains;
    }

    @Mapping(target = "name", source = "data.realm")
    DomainDTO createDomain(RealmRepresentation data, String issuer);

    @Mapping(target = "removeDomainsItem", ignore = true)
    ProviderDTO createProvider(String name, String description, String displayName, boolean fromToken,
            List<DomainDTO> domains);

    RoleSearchCriteria map(RoleSearchCriteriaDTO roleSearchCriteriaDTO);

    @Mapping(target = "removeStreamItem", ignore = true)
    RolePageResultDTO map(PageResult<RoleRepresentation> result);

    default UserRolesResponseDTO map(List<RoleRepresentation> userRoles) {
        UserRolesResponseDTO responseDTO = new UserRolesResponseDTO();
        responseDTO.setRoles(mapRoles(userRoles));
        return responseDTO;
    }

    List<RoleDTO> mapRoles(List<RoleRepresentation> roles);

    UserSearchCriteria map(UserSearchCriteriaDTO dto);

    @Mapping(target = "removeStreamItem", ignore = true)
    UserPageResultDTO map(PageResult<UserRepresentation> usersPage, @Context String domain, @Context String provider);

    @Mapping(target = "removeAttributesItem", ignore = true)
    @Mapping(target = "domain", expression = "java(domain)")
    @Mapping(target = "provider", expression = "java(provider)")
    UserDTO map(UserRepresentation user, @Context String domain, @Context String provider);

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
