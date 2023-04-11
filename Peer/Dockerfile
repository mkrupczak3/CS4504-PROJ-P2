FROM eclipse-temurin:11

COPY . /app
RUN /app/build.bash
ENTRYPOINT ["/app/run.bash"]
