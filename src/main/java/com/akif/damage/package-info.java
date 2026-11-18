@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"shared", "auth::api", "car::api", "rental::api", "payment::api"}
)
package com.akif.damage;
