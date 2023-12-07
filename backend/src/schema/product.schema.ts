import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import mongoose from 'mongoose';

@Schema({
    timestamps: {
        createdAt: 'time',
        currentTime: () => new Date(),
        updatedAt: false,
    },
})
export class ProductPrice {
    @Prop()
    productId: string;

    @Prop({ type: mongoose.Schema.Types.Date })
    time: Date;

    @Prop()
    price: number;

    @Prop()
    isSoldOut: boolean;
}

export const ProductPriceSchema = SchemaFactory.createForClass(ProductPrice);
