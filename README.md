# Vernacular VNC

Vernacular is a pure Java VNC remote desktop client library. Vernacular is open-source and
released under the MIT license.

## Getting Started

Vernacular is available through Maven:

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>io.xpipe</groupId>
        <artifactId>vernacular</artifactId>
        <version>1.16</version>
    </dependency>
</dependencies>
```

### Gradle
```groovy
repositories {
    mavenCentral()
}

dependencies {
    compile 'io.xpipe:vernacular:1.16'
}
```

## Usage

The Vernacular .jar file is executable, and it can be used as a simple but functional VNC client. However, Vernacular
is primarily intended to be used as a library in third-party applications: 

```java
package com.shinyhut.vernacular;

import com.shinyhut.vernacular.client.VernacularClient;
import com.shinyhut.vernacular.client.VernacularConfig;
import com.shinyhut.vernacular.client.rendering.ColorDepth;

public class VernacularDemo {

    public static void main(String[] args) {

        VernacularConfig config = new VernacularConfig();
        VernacularClient client = new VernacularClient(config);

        // Set up callbacks for the various events that can happen in a VNC session

        // Exception handler
        config.setErrorListener(Throwable::printStackTrace);

        // Password supplier - this is only invoked if the remote server requires authentication
        config.setPasswordSupplier(() -> "my secret password");

        // Handle system bell events from the remote host
        config.setBellListener(v -> System.out.println("DING!"));

        // Receive content copied to the remote clipboard
        config.setRemoteClipboardListener(text -> System.out.println(String.format("Received copied text: %s", text)));

        // Receive screen updates from the remote host
        // The 'image' parameter is a com.shinyhut.vernacular.client.rendering.ImageBuffer containing a current snapshot of the remote desktop
        // Expect this event to be triggered several times per second
        config.setScreenUpdateListener(image -> {
            int width = image.getWidth();
            int height = image.getHeight();
            System.out.println(String.format("Received a %dx%d screen update", width, height));
        });

        try {
            // Start the VNC session
            client.start("localhost", 5900);

            // Move the mouse
            client.moveMouse(400, 300);

            // Click a mouse button. Buttons are numbered 1 - 3
            client.click(1);

            // Type some text.
            client.type("Hello world!");

            // Copy some text to the remote clipboard
            client.copyText("Hello from the VNC client!");

            // Let the VNC session continue as long as required
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {
            }
        } finally {
            // Terminate the VNC session and cleanup
            client.stop();
        }
    }
}
```

For a more realistic example, see [Vernacular Viewer](/src/main/java/com/shinyhut/vernacular/VernacularViewer.java) in the source distribution, which demonstrates how to use Vernacular to build a working remote desktop application.
