REGISTRY?=nkijak
TAG?=latest

docker/monolith: Dockerfile
	docker buildx build \
		--platform linux/arm/v7 \
		-t monolith \
		--build-arg VCS_REF=todo \
		--build-arg BUILD_DATE=$$(date +%Y%m%d.%s) \
		--load \
		faas

docker/kafka-proxy: kafka-proxy/Dockerfile
	docker build \
		-t registry.kinnack.com/departurevision/kafka-proxy:${TAG} \
		--build-arg VCS_REF=todo \
		--build-arg BUILD_DATE=$$(date +%Y%m%d.%s) \
		kafka-proxy

push/monolith: 
	docker tag monolith registry.kinnack.com/departurevision/monolith:${TAG}
	docker push registry.kinnack.com/departurevision/monolith:${TAG}

push/kafka-proxy:
	docker push registry.kinnack.com/departurevision/kafka-proxy:${TAG}


NAMESPACE?=departure-vision-recorder
k8s/namespace:
	kubectl create namespace ${NAMESPACE} > k8s/namespace


k8s-deploy: k8s/namespace k8s/couchdb.yml k8s/dvr.yml k8s/kafka-proxy k8s/cronjob.yml

k8s/kafka-proxy: 
	kubectl apply -n ${NAMESPACE} -f $@.yml

k8s/%: 
	cat $@.yml | linkerd inject - | kubectl apply -n ${NAMESPACE} -f -

.PHONY: docker/% k8s/%

