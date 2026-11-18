@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"shared", "auth::api", "car::api", "currency::api", "payment::api"}
)
package com.akif.rental;
