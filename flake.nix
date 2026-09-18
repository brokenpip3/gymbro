{
  description = "gymbro";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-26.05";
    flake-utils.url = "github:numtide/flake-utils?ref=11707dc2f618dd54ca8739b309ec4fc024de578b";
  };

  outputs =
    {
      nixpkgs,
      flake-utils,
      ...
    }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            allowUnfree = true;
            android_sdk.accept_license = true;
          };
        };

        androidCompositionMinimal = pkgs.androidenv.composeAndroidPackages {
          buildToolsVersions = [ "36.0.0" ];
          platformVersions = [ "36" ];
        };

        androidCompositionFull = pkgs.androidenv.composeAndroidPackages {
          buildToolsVersions = [ "36.0.0" ];
          platformVersions = [ "36" ];
          abiVersions = [ "x86_64" ];
          includeSystemImages = true;
          includeEmulator = true;
          systemImageTypes = [ "google_apis_playstore" ];
        };

        androidSdkMinimal = androidCompositionMinimal.androidsdk;
        androidSdkFull = androidCompositionFull.androidsdk;

        mkGymbroShell =
          {
            name,
            sdk,
            includeJdk ? true,
            includeSdk ? true,
            hasEmulator ? false,
            withGui ? false,
            useAapt2Override ? true,
            extraInputs ? [ ],
          }:
          pkgs.mkShell (
            {
              inherit name;

              nativeBuildInputs =
                with pkgs;
                [
                  just
                  gnumake
                  gradle
                ]
                ++ pkgs.lib.optional includeJdk jdk17
                ++ pkgs.lib.optional includeSdk sdk
                ++ extraInputs;

              buildInputs = pkgs.lib.optionals withGui (
                with pkgs;
                [
                  libGL
                  libpulseaudio
                  stdenv.cc.cc.lib
                  vulkan-loader
                  libX11
                  libXext
                  libXcursor
                  libXi
                  libXrender
                  libXtst
                ]
              );

              LD_LIBRARY_PATH = pkgs.lib.optionalString withGui (
                pkgs.lib.makeLibraryPath (
                  with pkgs;
                  [
                    libGL
                    libpulseaudio
                    stdenv.cc.cc.lib
                    vulkan-loader
                    libX11
                    libXext
                    libXcursor
                    libXi
                    libXrender
                    libXtst
                  ]
                )
              );

              shellHook = ''
                export PATH="${pkgs.lib.optionalString includeJdk "$JAVA_HOME/bin:"}${pkgs.lib.optionalString includeSdk "$ANDROID_HOME/platform-tools:${pkgs.lib.optionalString hasEmulator "$ANDROID_HOME/emulator:"}$ANDROID_HOME/build-tools/36.0.0:"}$PATH"
                export GRADLE_USER_HOME="$(git rev-parse --show-toplevel)/.gradle-home"
                mkdir -p "$GRADLE_USER_HOME"
                ${pkgs.lib.optionalString (includeSdk && useAapt2Override) ''
                  echo "android.aapt2FromMavenOverride=${sdk}/libexec/android-sdk/build-tools/36.0.0/aapt2" > "$GRADLE_USER_HOME/gradle.properties"
                ''}
              '';
            }
            // pkgs.lib.optionalAttrs includeJdk { JAVA_HOME = pkgs.jdk17.home; }
            // pkgs.lib.optionalAttrs includeSdk {
              ANDROID_HOME = "${sdk}/libexec/android-sdk";
              ANDROID_SDK_ROOT = "${sdk}/libexec/android-sdk";
            }
          );
      in
      {
        devShells.default = mkGymbroShell {
          name = "gymbro-dev";
          sdk = androidSdkFull;
          hasEmulator = true;
          withGui = true;
        };

        devShells.gymbro-ci = mkGymbroShell {
          name = "gymbro-ci";
          sdk = androidSdkMinimal;
        };

        devShells.gymbro-android = mkGymbroShell {
          name = "gymbro-android";
          sdk = androidSdkFull;
          hasEmulator = true;
        };
      }
    );
}
