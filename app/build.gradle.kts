plugins {
    // Utilisez cette syntaxe sans spécifier de version
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.projet.skilllearn"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.projet.skilllearn"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appAuthRedirectScheme"] = "com.projet.skilllearn"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    // Add packaging options to handle duplicate files
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
            excludes += "META-INF/DEPENDENCIES"
            
            // Handle duplicate files in Netty libraries
            pickFirsts += "META-INF/native-image/io.netty/**"
            pickFirsts += "META-INF/native/libnetty_transport_native_epoll_**"
            pickFirsts += "META-INF/maven/io.netty/**"
            pickFirsts += "META-INF/maven/com.google.code.gson/**"
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Ajoutez cette section pour résoudre l'erreur "Unknown Kotlin JVM target"
    kotlinOptions {
        jvmTarget = "11"
    }

    // Add a lint configuration
    lint {
        abortOnError = false
        warningsAsErrors = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.activity:activity-ktx:1.8.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.6.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-storage")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Retrofit & Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment:2.7.5")
    implementation("androidx.navigation:navigation-ui:2.7.5")

    // Lottie for animations
    implementation("com.airbnb.android:lottie:6.0.0")

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:3.11.2")
    testImplementation("org.mockito:mockito-inline:3.11.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    // Picasso pour le chargement d'images
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.google.android.exoplayer:exoplayer-core:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")

    // Remove duplicate and keep only the latest version
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")
    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation("com.squareup.picasso:picasso:2.8")
    // CardView
    implementation("androidx.cardview:cardview:1.0.0")
}