# pos-install-helper
this is an utily to download and unzip a file from a URL in a fixed path

## Compile
`mvnw clean package`

## Run
`java -cp target\InstallHelper.jar dev.yaque.pos.installhelper.InstallHelper [URL_TARGET] [option]`

|**Options**| Description |
|--|--|
| keep |If you want to keep download zip file otherwise it will be deleted
