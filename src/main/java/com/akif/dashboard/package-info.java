@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"shared", "car::api", "damage::api", "payment::api", "rental::api"}
)
package com.akif.dashboard;
