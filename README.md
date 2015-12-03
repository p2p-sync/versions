# Object Management and Versioning
[![Build Status](https://travis-ci.org/p2p-sync/versions.svg?branch=master)](https://travis-ci.org/p2p-sync/versions)
[![Coverage Status](https://coveralls.io/repos/p2p-sync/versions/badge.svg?branch=master&service=github)](https://coveralls.io/github/p2p-sync/versions?branch=master)

# Install
Use Maven to add this component as your dependency:

```xml

<repositories>
  <repository>
    <id>version-mvn-repo</id>
    <url>https://raw.github.com/p2p-sync/versions/mvn-repo/</url>
    <snapshots>
        <enabled>true</enabled>
        <updatePolicy>always</updatePolicy>
    </snapshots>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>org.rmatil.sync.version</groupId>
    <artifactId>sync-version</artifactId>
    <version>0.1-SNAPSHOT</version>
  </dependency>
</dependencies>

```

# Architectural Overview
[![Architectural Overview](https://cdn.rawgit.com/p2p-sync/versions/master/src/main/resources/img/architectural-overview.svg)](https://cdn.rawgit.com/p2p-sync/versions/master/src/main/resources/img/architectural-overview.svg)

This component is able to save hashes computed over the content of a file in a simple object store. Tracked content is stored in a configurable json index file (e.g. `index.json`). For each stored object, a further object is stored (the `PathObject`). To avoid building the same directory structure again for storing these `PathObjects`, a hash is computed for the path to the file resp. directory. This hash is then stored in the index file accordingly to the file name. 

See the following two JSON files as example: 

```javascript
// index.json

{
  "paths": {
    "someDir": "71d7bda783b5264550dd268fbda8fe4f47b07481743e4016e8906f1c4b08187a"
  }
}

```

```javascript
// PathObject for someDir 71/d7bda783b5264550dd268fbda8fe4f47b07481743e4016e8906f1c4b08187a/71d7bda783b5264550dd268fbda8fe4f47b07481743e4016e8906f1c4b08187a.json

{
  "name": "someDir",
  "path": "",
  "pathType": "DIRECTORY",
  "isShared": false,
  "sharers": [],
  "versions": [
    {
      "hash": null
    }
  ]
}

```

The object is stored in a directory named equally to the file hash. The first two characters of the hash build another directory structure to avoid slow operations on certain filesystems due to too many directories:

> Some filesystems slow down if you put too many files in the same directory; making the first byte of the SHA1 into a directory is an easy way to create a fixed, 256-way partitioning of the namespace for all possible objects with an even distribution
>
> -- <cite>Loeliger, J., & McCullough, M. (2012). Version Control with Git: Powerful Tools and Techniques for Collaborative Software Development.</cite>


# Configuration

```java

import org.rmatil.sync.persistence.api.IStorageAdapter;
import org.rmatil.sync.persistence.core.local.LocalStorageAdapter;
import org.rmatil.sync.version.api.IObjectManager;
import org.rmatil.sync.version.core.ObjectManager;
import org.rmatil.sync.version.api.IVersionManager;
import org.rmatil.sync.version.core.VersionManager;

import org.rmatil.sync.version.api.PathType;
import org.rmatil.sync.version.core.model.PathObject;
import org.rmatil.sync.version.core.model.Version;

import java.nio.file.Paths;

class Main {
  
  public static void main(String[] args) {
    // the root path in which the object store will be created
    Path rootPath = Paths.get("/tmp");
 
    // the folder in which the object store is placed
    IStorageAdapter objectStorageManager = new LocalStorageAdapter(rootPath.resolve(".sync"));
    // the first argument is the name of the index file, the second the name of the directory in which the PathObjects are stored
    IObjectManager objectManager = new ObjectManager("index.json", "object", objectStorageManager);
    IVersionManager versionManager = new new VersionManager(objectManager);
    
    // ...
    
    // Create a new path object for the file to add
    PathObject pathObject = new PathObject("fileName", "path/To/file", PathType.FILE, false, new ArrayList<>(), new ArrayList<>());
    // write pathObject
    objectManager.writeObject(pathObject);
    
    // create a new version of the file
    Version v1 = new Version("HashOfVersion1");
    
    versionManager
    versionManager.addVersion(v1, "path/To/file/fileName");
  }
}

```

# License

```
   Copyright 2015 rmatil

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

```
