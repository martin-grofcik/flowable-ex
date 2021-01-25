# flowable-shell

Shell application to manipulate Flowable objects. 
## Build
```
mvnw install -DskipTests=true
```
## Execute
```
java -jar target/flowable-shell-1.0.0-SNAPSHOT.jar
```
## Use
```shell script
shell:>help
AVAILABLE COMMANDS

Deployment
        delete-deployments, rmd: Delete all deployments with given name, tenantId from runtime. WARNING - use only for testing purposes
        deploy: Deploy given application
        list-deployments, lsd: list deployments

Model
        delete-model, rm: Delete model from modeler.
        export: Export model from modeler to file.
        export-bar: Export deployable model from modeler to file.
        import: Import file to modeler.
        list, ls: List models.

Utils
        configure: Configure flowable rest endpoint.
        unzip: Unzip file to directory.
        zip: Zip directory to file.
```

### Export application model from modeler and deploy to app
```shell script
shell:> export-bar --name app --output-file-name target/test/app.bar
shell:> unzip target/test/app.bar target/test/app
shell:> zip target/test/app target/test/app-out.bar
shell:> deploy target/test/app-out.bar
```