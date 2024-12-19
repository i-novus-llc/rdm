FROM harbor.i-novus.ru/library/redos7c/base:7.3-240812

USER root

RUN echo "install_weak_deps=False" >> /etc/dnf/dnf.conf && \
    dnf install --nodocs -y \
    jq-1.5-3.el7 dos2unix && \
    rm -rf /var/cache/dnf /var/log/dnf* /var/tmp/* /tmp/* && \
    dnf clean all

COPY --chown=10001 startup.sh ./
COPY startup.sh ./

RUN chmod +x startup.sh && dos2unix startup.sh

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

USER 10001

ENTRYPOINT ["/bin/sh", "startup.sh"]