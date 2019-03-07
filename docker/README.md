# LAUNCH UPDATE-SERVER

```$shell
$docker-compuse up
```

# DATA
- Tenant: Default
- Port: 8081

## UI
- username: test
- password: test

## SOFTWARE MODULES
- os: type os, 1 file named test_4
- app: type app, 1 file named test_1
- apps: type app, 2 file named test_2, test_3

## DISTRIBUTIONS
- osWithApps: type os with apps, with app, apps and os software module
- osOnly: type os, with software module named os
- app: type app, with softwrae module named app

## DDI SECURITY:
- Gataway token: e3d458bb8328c09dcce94ba8b7078ea4
- Target1 target token: dcb29877211ec01b195c2786ad773608
- Target2 target token: 9cb5d32400d0bc22d18eae55b62086a2
- Target3 target token: 57c60decca852cacfc6e318626a3d542

## Server request:
- Target1 server request: 
  * put target metadata
  * cancel update osOnly;
  * apply update app;
- Target2 server request:
  * put target metadata
  * apply update app
- Target3 server request:
  * put target metadata
  * apply osWithApps
