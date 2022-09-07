<!-- Improved compatibility of back to top link: See: https://github.com/othneildrew/Best-README-Template/pull/73 -->
<a name="readme-top"></a>
<!--
*** Thanks for checking out the Best-README-Template. If you have a suggestion
*** that would make this better, please fork the repo and create a pull request
*** or simply open an issue with the tag "enhancement".
*** Don't forget to give the project a star!
*** Thanks again! Now go create something AMAZING! :D
-->



<!-- PROJECT SHIELDS -->
<!--
*** I'm using markdown "reference style" links for readability.
*** Reference links are enclosed in brackets [ ] instead of parentheses ( ).
*** See the bottom of this document for the declaration of the reference variables
*** for contributors-url, forks-url, etc. This is an optional, concise syntax you may use.
*** https://www.markdownguide.org/basic-syntax/#reference-style-links
-->
[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![License: GPL v3][license-shield]][license-url]
[![Javadoc][javadoc-shield]][javadoc-url]
[![MavenCentral][maven-shield]][maven-url]




<!-- PROJECT LOGO -->
<br />
<div align="center">
  <!-- a href="https://github.com/mlgr-io/kotlin-whois-parser">
    <img src="images/logo.png" alt="Logo" width="80" height="80">
  </a //-->

<h3 align="center">kotlin-whois-parser</h3>

  <p align="center">
    A library for WHOIS response parsing. 
    <!-- br />
    <a href="https://github.com/mlgr-io/kotlin-whois-parser"><strong>Explore the docs »</strong></a //-->
    <br />
    <br />
    <!-- a href="https://github.com/mlgr-io/kotlin-whois-parser">View Demo</a>
    · //-->
    <a href="https://github.com/mlgr-io/kotlin-whois-parser/issues">Report Bug</a>
    ·
    <a href="https://github.com/mlgr-io/kotlin-whois-parser/issues">Request Feature</a>
  </p>
</div>



<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li><a href="#about-the-project">About The Project</a></li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
  </ol>
</details>



<!-- ABOUT THE PROJECT -->
## About The Project

**kotlin-whois-parser** is a library for requesting and parsing data from (primary) WHOIS servers into reusable models.

As of August 2022, the Wikipedia states [1,502 active top level domains](https://en.wikipedia.org/wiki/List_of_Internet_top-level_domains),
including [308 country-code top level domains](https://en.wikipedia.org/wiki/Country_code_top-level_domain).
Due to the existence of hundreds of NICs with their wide range of different data output (some of them doesn't
provide any information at all), it's hard for developers to gather useful and structured information from WHOIS.

Since there was no parser library for Java or Kotlin available as of Summer 2022 (at least we didn't find one), this
project was started and gears towards all those use-cases one would like to get information from public WHOIS databases.


Therefor, this library makes use of the [WhoisClient](https://commons.apache.org/proper/commons-net/apidocs/org/apache/commons/net/whois/WhoisClient.html)
from [org.apache.commons.net](https://commons.apache.org/proper/commons-net/) for the requests itself, and parses the
output with [Parsing expression grammars](https://en.wikipedia.org/wiki/Parsing_expression_grammar) (via
[parboiled](https://github.com/sirthias/parboiled/wiki)) afterwards (you may find all available parsers
[here](src/main/kotlin/io/mailguru/whois/parser/impl/)).

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- GETTING STARTED -->
## Getting Started

<!-- [Javadoc](https://javadoc.io/doc/io.mailguru/whois-parser) -->

### Prerequisites

We choose to support the lowest actively supported Java version at the time of writing, that is, Java 11 (this may be
subject to change in future major releases). There are no additional dependencies other than Kotlin and some plugins
for publishing and documentation (Dokka).

### Installation

There are several ways to install this library:

1. Apache Maven
   ```
   <dependency>
     <groupId>io.mailguru</groupId>
     <artifactId>whois-parser</artifactId>
     <version>1.0.0</version>
   </dependency>
   ```
2. Gradle Groovy DSL
   ```
   implementation 'io.mailguru:whois-parser:1.0.0'
   ```
3. Gradle Kotlin DSL
   ```
   implementation("io.mailguru:whois-parser:1.0.0")
   ```
4. Or you may clone the latest branch of your choice (presumably this will be `develop` ) of this repository and work on
   that clone itself or publish it to your local maven repository: 
   ```sh
   git clone https://github.com/mlgr-io/kotlin-whois-parser.git
   cd kotlin-whois-parser
   ./gradlew publishToMavenLocal
   ```
   Then, in your target project, import the local lib by one of the methods (1. - 3.) above. Please make sure that the `version`
   you import matches the value given in your local `build.gradle` and you local maven repository:
   ```sh
   repositories {
        mavenLocal()
        // ...
   }
   ```
 

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- USAGE EXAMPLES -->
## Usage

Gather information is pretty easy. Your entry point is the [WhoisService](src/main/kotlin/io/mailguru/whois/service/WhoisService.kt)
singleton class (that is, `object` in Kotlin), that you use to make a call to the `lookup()` method with the hostname
in question as its argument:

* in **Kotlin**:
  ```kotlin
  import io.mailguru.whois.service.WhoisService
  import io.mailguru.whois.model.WhoisResult

  // ...
  val result: WhoisResult = WhoisService.lookup("example.com")
  // ...

  ```
* in **Java**:
  ```java
  import io.mailguru.whois.service.WhoisService;
  import io.mailguru.whois.model.WhoisResult;

  // ...
  WhoisResult result = WhoisService.INSTANCE.lookup("example.com");
  // ...
  ```
  
You will then, on a successful pass, have a [WhoisResult](src/main/kotlin/io/mailguru/whois/model/WhoisResult.kt) object
containing the parsed data.
Please be aware that exceptions may be thrown by this method; see [latest Javadoc](https://javadoc.io/doc/io.mailguru/whois-parser/latest/whois-parser/io.mailguru.whois.service/-whois-service/lookup.html)
or [the code itself](src/main/kotlin/io/mailguru/whois/service/WhoisService.kt#L77) for details.


<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- CONTRIBUTING -->
## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

This library uses [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- LICENSE -->
## License

Distributed under the **GNU General Public License v3.0**. See [LICENSE.md](LICENSE.md) for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- CONTACT -->
## Contact

Project Link: [https://github.com/mlgr-io/kotlin-whois-parser](https://github.com/mlgr-io/kotlin-whois-parser)

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[javadoc-url]: https://javadoc.io/doc/io.mailguru/whois-parser
[javadoc-shield]: https://javadoc.io/badge2/io.mailguru/whois-parser/javadoc.svg?style=for-the-badge&color=yellow
[maven-url]: https://search.maven.org/artifact/io.mailguru/whois-parser
[maven-shield]: https://img.shields.io/maven-central/v/io.mailguru/whois-parser?style=for-the-badge
[contributors-shield]: https://img.shields.io/github/contributors/mlgr-io/kotlin-whois-parser.svg?style=for-the-badge
[contributors-url]: https://github.com/mlgr-io/kotlin-whois-parser/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/mlgr-io/kotlin-whois-parser.svg?style=for-the-badge
[forks-url]: https://github.com/mlgr-io/kotlin-whois-parser/network/members
[stars-shield]: https://img.shields.io/github/stars/mlgr-io/kotlin-whois-parser.svg?style=for-the-badge
[stars-url]: https://github.com/mlgr-io/kotlin-whois-parser/stargazers
[issues-shield]: https://img.shields.io/github/issues/mlgr-io/kotlin-whois-parser.svg?style=for-the-badge
[issues-url]: https://github.com/mlgr-io/kotlin-whois-parser/issues
[license-shield]: https://img.shields.io/github/license/mlgr-io/kotlin-whois-parser.svg?style=for-the-badge
[license-url]: https://github.com/mlgr-io/kotlin-whois-parser/blob/master/LICENSE.md
[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555
[linkedin-url]: https://linkedin.com/in/linkedin_username
[product-screenshot]: images/screenshot.png
[Next.js]: https://img.shields.io/badge/next.js-000000?style=for-the-badge&logo=nextdotjs&logoColor=white
[Next-url]: https://nextjs.org/
[React.js]: https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB
[React-url]: https://reactjs.org/
[Vue.js]: https://img.shields.io/badge/Vue.js-35495E?style=for-the-badge&logo=vuedotjs&logoColor=4FC08D
[Vue-url]: https://vuejs.org/
[Angular.io]: https://img.shields.io/badge/Angular-DD0031?style=for-the-badge&logo=angular&logoColor=white
[Angular-url]: https://angular.io/
[Svelte.dev]: https://img.shields.io/badge/Svelte-4A4A55?style=for-the-badge&logo=svelte&logoColor=FF3E00
[Svelte-url]: https://svelte.dev/
[Laravel.com]: https://img.shields.io/badge/Laravel-FF2D20?style=for-the-badge&logo=laravel&logoColor=white
[Laravel-url]: https://laravel.com
[Bootstrap.com]: https://img.shields.io/badge/Bootstrap-563D7C?style=for-the-badge&logo=bootstrap&logoColor=white
[Bootstrap-url]: https://getbootstrap.com
[JQuery.com]: https://img.shields.io/badge/jQuery-0769AD?style=for-the-badge&logo=jquery&logoColor=white
[JQuery-url]: https://jquery.com
