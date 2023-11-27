package app.priceguard.data.repository

import app.priceguard.data.dto.PricePatchRequest
import app.priceguard.data.dto.PricePatchResponse
import app.priceguard.data.dto.ProductAddRequest
import app.priceguard.data.dto.ProductAddResponse
import app.priceguard.data.dto.ProductData
import app.priceguard.data.dto.ProductDeleteState
import app.priceguard.data.dto.ProductDetailResult
import app.priceguard.data.dto.ProductDetailState
import app.priceguard.data.dto.ProductListResult
import app.priceguard.data.dto.ProductListState
import app.priceguard.data.dto.ProductVerifyRequest
import app.priceguard.data.dto.ProductVerifyResponse
import app.priceguard.data.dto.RecommendProductData
import app.priceguard.data.dto.RecommendProductResult
import app.priceguard.data.dto.RecommendProductState
import app.priceguard.data.dto.RenewResult
import app.priceguard.data.network.APIResult
import app.priceguard.data.network.ProductAPI
import app.priceguard.data.network.getApiResult
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val productAPI: ProductAPI,
    private val tokenRepository: TokenRepository
) : ProductRepository {

    override suspend fun verifyLink(productUrl: ProductVerifyRequest): APIResult<ProductVerifyResponse> {
        val response = getApiResult {
            productAPI.verifyLink(productUrl)
        }
        when (response) {
            is APIResult.Success -> {
                return response
            }

            is APIResult.Error -> {
                return when (response.code) {
                    400 -> {
                        response
                    }

                    401 -> {
                        response
                    }

                    else -> {
                        response
                    }
                }
            }
        }
    }

    override suspend fun addProduct(productAddRequest: ProductAddRequest): APIResult<ProductAddResponse> {
        val response = getApiResult {
            productAPI.addProduct(productAddRequest)
        }
        when (response) {
            is APIResult.Success -> {
                return response
            }

            is APIResult.Error -> {
                return when (response.code) {
                    400 -> {
                        response
                    }

                    401 -> {
                        response
                    }

                    else -> {
                        response
                    }
                }
            }
        }
    }

    override suspend fun getProductList(afterRenew: Boolean): ProductListResult {
        val response = getApiResult {
            productAPI.getProductList()
        }
        when (response) {
            is APIResult.Success -> {
                return ProductListResult(
                    ProductListState.SUCCESS,
                    response.data.trackingList?.map { dto ->
                        ProductData(
                            dto.productName ?: "",
                            dto.productCode ?: "",
                            dto.shop ?: "",
                            dto.imageUrl ?: "",
                            dto.targetPrice ?: 0,
                            dto.price ?: 0
                        )
                    } ?: listOf()
                )
            }

            is APIResult.Error -> {
                when (response.code) {
                    401 -> {
                        if (afterRenew) {
                            return ProductListResult(ProductListState.PERMISSION_DENIED, listOf())
                        } else {
                            val refreshToken =
                                tokenRepository.getRefreshToken() ?: return ProductListResult(
                                    ProductListState.PERMISSION_DENIED,
                                    listOf()
                                )
                            val renewResult = tokenRepository.renewTokens(refreshToken)
                            if (renewResult != RenewResult.SUCCESS) {
                                return ProductListResult(
                                    ProductListState.PERMISSION_DENIED,
                                    listOf()
                                )
                            }
                            return getProductList(afterRenew = true)
                        }
                    }

                    404 -> {
                        return ProductListResult(ProductListState.NOT_FOUND, listOf())
                    }

                    else -> {
                        return ProductListResult(ProductListState.UNDEFINED_ERROR, listOf())
                    }
                }
            }
        }
    }

    override suspend fun getRecommendedProductList(afterRenew: Boolean): RecommendProductResult {
        val response = getApiResult {
            productAPI.getRecommendedProductList()
        }
        when (response) {
            is APIResult.Success -> {
                return RecommendProductResult(
                    RecommendProductState.SUCCESS,
                    response.data.recommendList?.map { dto ->
                        RecommendProductData(
                            dto.productName ?: "",
                            dto.productCode ?: "",
                            dto.shop ?: "",
                            dto.imageUrl ?: "",
                            dto.price ?: 0,
                            dto.rank ?: 0
                        )
                    } ?: listOf()
                )
            }

            is APIResult.Error -> {
                return when (response.code) {
                    400 -> {
                        RecommendProductResult(RecommendProductState.WRONG_REQUEST, listOf())
                    }

                    401 -> {
                        if (afterRenew) {
                            return RecommendProductResult(
                                RecommendProductState.PERMISSION_DENIED,
                                listOf()
                            )
                        } else {
                            val refreshToken =
                                tokenRepository.getRefreshToken() ?: return RecommendProductResult(
                                    RecommendProductState.PERMISSION_DENIED,
                                    listOf()
                                )
                            val renewResult = tokenRepository.renewTokens(refreshToken)
                            if (renewResult != RenewResult.SUCCESS) {
                                return RecommendProductResult(
                                    RecommendProductState.PERMISSION_DENIED,
                                    listOf()
                                )
                            }
                            return getRecommendedProductList(afterRenew = true)
                        }
                    }

                    404 -> {
                        RecommendProductResult(RecommendProductState.NOT_FOUND, listOf())
                    }

                    else -> {
                        RecommendProductResult(RecommendProductState.UNDEFINED_ERROR, listOf())
                    }
                }
            }
        }
    }

    override suspend fun getProductDetail(
        productCode: String,
        renewed: Boolean
    ): ProductDetailResult {
        when (val response = getApiResult { productAPI.getProductDetail(productCode) }) {
            is APIResult.Success -> {
                return ProductDetailResult(
                    ProductDetailState.SUCCESS,
                    productName = response.data.productName ?: "",
                    productCode = response.data.productCode,
                    shop = response.data.shop,
                    imageUrl = response.data.imageUrl,
                    rank = response.data.rank,
                    shopUrl = response.data.shopUrl,
                    targetPrice = response.data.targetPrice,
                    lowestPrice = response.data.lowestPrice,
                    price = response.data.price
                )
            }

            is APIResult.Error -> {
                when (response.code) {
                    401 -> {
                        if (renewed) {
                            return ProductDetailResult(ProductDetailState.PERMISSION_DENIED)
                        }

                        val refreshToken =
                            tokenRepository.getRefreshToken() ?: return ProductDetailResult(
                                ProductDetailState.PERMISSION_DENIED
                            )

                        val renewResult = tokenRepository.renewTokens(refreshToken)

                        if (renewResult != RenewResult.SUCCESS) {
                            return ProductDetailResult(ProductDetailState.PERMISSION_DENIED)
                        }

                        return getProductDetail(productCode, true)
                    }

                    404 -> {
                        return ProductDetailResult(ProductDetailState.NOT_FOUND)
                    }

                    else -> {
                        return ProductDetailResult(ProductDetailState.UNDEFINED_ERROR)
                    }
                }
            }
        }
    }

    override suspend fun deleteProduct(productCode: String, renewed: Boolean): ProductDeleteState {
        when (val response = getApiResult { productAPI.deleteProduct(productCode) }) {
            is APIResult.Success -> {
                return ProductDeleteState.SUCCESS
            }

            is APIResult.Error -> {
                when (response.code) {
                    400 -> {
                        return ProductDeleteState.INVALID_REQUEST
                    }

                    401 -> {
                        if (renewed) {
                            return ProductDeleteState.UNAUTHORIZED
                        }

                        val refreshToken = tokenRepository.getRefreshToken()
                            ?: return ProductDeleteState.UNAUTHORIZED
                        val renewResult = tokenRepository.renewTokens(refreshToken)

                        if (renewResult != RenewResult.SUCCESS) {
                            return ProductDeleteState.UNAUTHORIZED
                        }

                        return deleteProduct(productCode, true)
                    }

                    404 -> {
                        return ProductDeleteState.NOT_FOUND
                    }

                    else -> {
                        return ProductDeleteState.UNDEFINED_ERROR
                    }
                }
            }
        }
    }

    override suspend fun updateTargetPrice(pricePatchRequest: PricePatchRequest): APIResult<PricePatchResponse> {
        val response = getApiResult {
            productAPI.updateTargetPrice(pricePatchRequest)
        }
        return response
    }
}
