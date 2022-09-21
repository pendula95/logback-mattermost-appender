# Logback Mattermost Appender

[![CI](https://github.com/pendula95/logback-mattermost-appender/actions/workflows/maven-build.yml/badge.svg)](https://github.com/pendula95/logback-mattermost-appender/actions/workflows/maven-build.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.pendula95/logback-mattermost-appender/badge.svg)](io.github.pendula95/logback-mattermost-appender)

<img src="https://repository-images.githubusercontent.com/538599152/f83f0ed4-ec4b-420a-878e-e1003196d4cf" width="480" height="240"> 

LMA is a simple [Logback](https://logback.qos.ch/) appender which provides a way to send log messages directly to
your [Mattermost](https://mattermost.com/) server. It supports both BOT integration and Webhook integration.

This version of LMA has been tested against the latest Mattermost server version.

Binaries/Download
----------------
Binaries and dependency information for Maven, Ivy, Gradle and others can be found at http://search.maven.org.

Releases of lettuce are available in the Maven Central repository.

Example for Maven:

```xml

<dependency>
    <groupId>io.github.pendula95</groupId>
    <artifactId>logback-mattermost-appender</artifactId>
    <version>x.y.z</version>
</dependency>
```

Example for Gradle:

```groovy
implementation 'io.github.pendula95:logback-mattermost-appender:x.y.z'
```

Basic Usage
-----------
Add SlackAppender configuration to logback.xml file.

Bot integration Appender configuration:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<configuration>
    ...
    <appender name="MATTERMOST" class="io.github.pendula95.logback.MattermostAppender">
        <!-- Address of your mattermost server - MANDATORY -->
        <serverAddress>https://messages.acme.com</serverAddress>
        <!-- Mattermost BOT API token - MANDATORY-->
        <token>xxxxxxxxxxxxxxxxxxxxxxxxxx</token>
        <!-- ChannelId that you want to post - MANDATORY -->
        <channelId>yyyyyyyyyyyyyyyyyyyyyyyyyy</channelId>

        <!-- If color coding of log levels should be used -->
        <colorCoding>true</colorCoding>
    </appender>
</configuration>
```

Webhook integration example:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<configuration>
    ...
    <appender name="MATTERMOST" class="io.github.pendula95.logback.MattermostAppender">
        <!-- Mattermost incoming webhook uri - MANDATORY -->
        <webhook>https://messages.acme.com/hooks/xxxxxxxxxxxxxxxxxxxxxxxxxx</webhook>

        <!-- Channel that you want to post. Defaults to the channel set during webhook creation. -->
        <!-- <channel>town-square</channel> -->

        <!-- Overrides the username the message posts as. 
        Defaults to the username set during webhook creation or the webhook creator’s username if the former was not set. -->
        <!-- <username></username> -->

        <!-- Overrides the profile picture the message posts with -->
        <!-- <iconUrl></iconUrl> -->
        
        <!-- Overrides the profile picture and icon_url parameter.
        Defaults to none and is not set during webhook creation. -->
        <!-- <icon_emoji>:sign_of_the_horns:</icon_emoji> -->
        
        <!-- If color coding of log levels should be used -->
        <colorCoding>true</colorCoding>
    </appender>
</configuration>
```

Recommended way of using Mattermost appender:

```xml

<configuration>
    ....
    <!-- Currently recommended way of using Mattermost appender -->
    <appender name="ASYNC_MATTERMOST" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="MATTERMOST"/>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
    </appender>
    <root>
        <level value="ALL"/>
        <appender-ref ref="ASYNC_MATTERMOST"/>
    </root>
</configuration>
```

Example
-------
<img src="https://lazarbulic.com/lma_example.png" width="810" height="240">

References
----------
* [Mattermost Webhooks](https://developers.mattermost.com/integrate/webhooks/incoming/)
* [Mattermost API](https://api.mattermost.com/)