package org.exoplatform.webconferencing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActiveProviderInfo {

    private String name;

    private String url;

    private boolean heavyConnector;
}
