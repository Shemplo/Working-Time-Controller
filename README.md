# Working time controller
> Simple controller of your working time

### How to use it

1. add `dependencies` to maven with 
`mvn istall:install-file -Dfile=<path> -DgroupId=ru.shemplo -DartifactId=dsau -Dversion=1.0.0 -Dpackaging=jar -DgeneratePom=true` 
1. build project with `mvn clean package`
1. run built `jar` file in `target` directory (`java -jar wtc-[version].jar`)
1. click on `Project name` and in opened window define path to the project directory
1. change something in project and see how your time is running

### Preview

| Project is not selected            | Project is not active              | Project is changed                |
|:----------------------------------:|:----------------------------------:|:---------------------------------:|
| ![local 1](screenshots/local1.png) | ![local 2](screenshots/local2.png) | ![local 3](screenshots/local3.png)|
