# Publishing to GitHub

Suggested repository name: `matchday`

Suggested description:

> Football league management web app built with Java 25, Spring Boot, Spring Security, JPA, MariaDB and Thymeleaf.

Suggested topics: `java`, `spring-boot`, `spring-security`, `spring-data-jpa`, `thymeleaf`, `mariadb`, `football`, `mvc`.

Create an empty public repository on GitHub. Do not initialize it with another README, .gitignore or license. From the extracted project root:

```bash
git init -b main
git add .
git update-index --chmod=+x mvnw
git diff --cached --stat
git status --short
git commit -m "Prepare Matchday for public release"
git remote add origin https://github.com/YOUR_USERNAME/matchday.git
git push -u origin main
```

Replace `YOUR_USERNAME` with your account name. The executable flag allows the Maven wrapper to run directly after a Linux checkout. No credentials, remote configuration or previous Git history are included in the prepared archive.

The intended public files include `.env.example`, `.github` and `.mvn`; local `.env` files and `target/` must stay untracked. Use the Git commands above rather than uploading the ZIP itself.

After publishing, check the Actions tab for the first build result. The workflow is prepared locally; its first hosted execution happens only after you push.

No new license has been assigned during packaging. Keep the existing authorship credit; select a license separately if you want to grant reuse permissions.
