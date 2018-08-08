set -e
./gradlew bintrayUpload -PdeployCore=true

./gradlew bintrayUpload -PdeployGradle=true
./gradlew bintrayUpload -PdeployMigrations=true

./gradlew bintrayUpload -PdeployRuntimeCommon=true
./gradlew bintrayUpload -PdeployRuntimeNative=true
./gradlew bintrayUpload -PdeployRuntimeJdk=true

./gradlew bintrayUpload -PdeployMultiplatformCommon=true
./gradlew bintrayUpload -PdeployMultiplatformIos=true
./gradlew bintrayUpload -PdeployMultiplatformAndroid=true