# How to make a new version

- If you want to create a new version you should have access to the Bintray repository of eqasim

## Which new version number to choose?

- We follow `semver`, which means `MAJOR.MINOR.PATCH`
- `PATCH` is increased if bugs got fixed and no interfaces have been changed.
- `MINOR` is updated when interfaces change, new functionality is added.
- `MAJOR` is updated when major changes have been done (whatever this means exactly :)

## Step by step

- Create a new branch `version:X.X.X` based on the current `develop`
- In the new branch, update the version in the Maven packages by
  - `mvn versions:set` and then giving the new version number `X.X.X`
  - `mvn versions:commit` to accept the preliminary changes (which you can verify) in the `pom.xml`
- Update all the references to the old version `Y.Y.Y` with the new version `X.X.X` in `README.md`
- Update the `CHANGELOG.md` 
  - ... by replacing **Development version** with **X.X.X**
  - ... by adding a new section **Development version** on top of the changelog and one bullet point "No changes yet"
- Commit the changes to the new branch
- Create a PR on Github and wait until the test have passed.
- Merge the PR on Github in case there are no errors. Call the PR "Release X.X.X".
- Locally, check out the updated `develop` branch
- Run `git tag vX.X.X` to create the new verison tag
- Run `git push --tags` to push the new version tag
- Run `mvn deploy` to deploy the Maven artifacts to Bintray

## What's next?

- Bintray will soon stop its public hosting of open source projects.
- An option is to move to Github packages, but it is not ideal as at some point we will have thousands of repositories in the `pom.xml` files. Let's see how MATSim will proceed with that.
