@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"shared", "auth::api", "damage::api", "payment::api", "rental::api"}
)
package com.akif.notification;
