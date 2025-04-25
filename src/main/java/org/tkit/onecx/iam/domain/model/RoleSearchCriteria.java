package org.tkit.onecx.iam.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleSearchCriteria {

    private String name;

    private Integer pageNumber = 0;

    private Integer pageSize = 100;
}
