package org.ffpy.portmux.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import org.ffpy.portmux.config.ConfigManager
import org.ffpy.portmux.config.ForwardConfigManager
import org.ffpy.portmux.util.AddressUtils
import org.slf4j.LoggerFactory

/**
 * 转发服务
 */
object ForwardServer {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 启动服务
     */
    fun start() {
        val boosGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()
        try {
            val future = ServerBootstrap()
                .group(boosGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast("matchHandler", MatchHandler(ForwardConfigManager.forwardConfig))
                    }
                })
                .bind(AddressUtils.parseAddress(ConfigManager.config.listen))
                .sync()

            log.info("启动转发服务: ${ConfigManager.config.listen}")

            future.channel().closeFuture().sync()
        } finally {
            boosGroup.shutdownGracefully().syncUninterruptibly()
            workerGroup.shutdownGracefully().syncUninterruptibly()
        }
    }
}