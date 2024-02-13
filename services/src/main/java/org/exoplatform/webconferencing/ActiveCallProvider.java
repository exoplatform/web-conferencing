package org.exoplatform.webconferencing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActiveCallProvider {

    private String name;

    private String url;

    private boolean integratedConnector;
}
