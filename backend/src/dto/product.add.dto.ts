import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsNumber, IsString, Max } from 'class-validator';
import { MAX_TARGET_PRICE } from 'src/constants';

export class ProductAddDto {
    @ApiProperty({
        example: '5897533626',
        description: '상품 코드',
        required: true,
    })
    @IsString()
    @IsNotEmpty()
    productCode: string;
    @ApiProperty({
        example: 36000,
        description: '목표 가격',
        required: true,
    })
    @IsNumber()
    @Max(MAX_TARGET_PRICE)
    @IsNotEmpty()
    targetPrice: number;
}

export class ProductAddDtoV1 {
    @ApiProperty({
        example: 'smartStore',
        description: '쇼핑몰 정보',
        required: true,
    })
    @IsString()
    @IsNotEmpty()
    shop: string;
    @ApiProperty({
        example: '5897533626',
        description: '상품 코드',
        required: true,
    })
    @IsString()
    @IsNotEmpty()
    productCode: string;
    @ApiProperty({
        example: 36000,
        description: '목표 가격',
        required: true,
    })
    @IsNumber()
    @Max(MAX_TARGET_PRICE)
    @IsNotEmpty()
    targetPrice: number;
}
